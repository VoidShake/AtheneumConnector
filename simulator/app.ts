import { v4 as createUUID } from "uuid";
import { loadCache, saveCache } from "./cache";
import type { Point } from "./messages";
import { publish, subscribe } from "./rabbitmq";

const players = loadCache();
const goals = new Map<string, Point>();
const speeds = new Map<string, number>();

const MAX_PLAYERS = 20;

console.log("simulator started");

function addPlayer() {
  const uuid = createUUID();
  console.log(`${uuid} joined the game (${players.size}/${MAX_PLAYERS})`);
  const pos = randomPos();
  players.set(uuid, pos);
  publish("PlayerMoveMessage", { pos, player: uuid });
}

function removePlayer(uuid: string) {
  console.log(`${uuid} left the game (${players.size}/${MAX_PLAYERS})`);
  players.delete(uuid);
  publish("PlayerDisconnectMessage", { player: uuid });
}

if (players.size === 0) {
  for (let i = 0; i < MAX_PLAYERS / 2; i++) {
    addPlayer();
  }
}

let interval = Number.parseInt(process.env.INTERVAL ?? "");
if (isNaN(interval)) interval = 100;

function randomPos(): Point {
  const x = Math.random() * 1000 - 500;
  const z = Math.random() * 1000 - 500;
  const y = 80;
  const world = "overworld";
  return { x, y, z, world };
}

function step(pos: Point, uuid: string) {
  let goal = goals.get(uuid);

  if (!goal) {
    if (Math.random() < 0.1) return removePlayer(uuid);
    goal = randomPos();
    goals.set(uuid, goal);
  }

  if (Math.random() > 0.2) return;

  const diff = {
    x: goal.x - pos.x,
    z: goal.z - pos.z,
  };

  const dist = Math.sqrt(Math.pow(diff.x, 2) + Math.pow(diff.x, 2));

  if (dist <= 1) {
    goals.delete(uuid);
    return;
  }

  const speed = speeds.get(uuid) ?? 0.1;

  if (Math.random() < 0.2) {
    speeds.set(uuid, Math.max(0, speed - 0.1));
  } else if (Math.random() > 0.5) {
    speeds.set(uuid, Math.min(3, speed + 0.1));
  }

  const vec = {
    x: (diff.x / dist) * speed,
    z: (diff.z / dist) * speed,
  };

  function add(a: number, b: number) {
    if (b < 0) return Math.floor(a + b);
    return Math.ceil(a + b);
  }

  const newPos: Point = {
    ...pos,
    x: add(pos.x, vec.x),
    z: add(pos.z, vec.z),
  };

  publish("PlayerMoveMessage", { pos: newPos, player: uuid });

  players.set(uuid, newPos);
}

setInterval(() => {
  players.forEach(step);

  if (players.size < MAX_PLAYERS && Math.random() < 0.5) {
    addPlayer();
  }

  saveCache(players);
}, interval);

publish("ServerStatusMessage", { status: "STARTED" });

subscribe("RestartMessage", () => {
  publish("ServerStatusMessage", { status: "RECOVERED" });
});

function shutdown() {
  console.log("shutting down simulator");

  players.forEach((_, player) => {
    publish("PlayerDisconnectMessage", { player });
  });

  publish("ServerStatusMessage", { status: "STOPPED" });
}

process.on("exit", shutdown);
process.on("SIGINT", shutdown);
process.on("SIGUSR1", shutdown);
process.on("SIGUSR2", shutdown);
process.on("SIGTERM", shutdown);
process.on("SIGBREAK", shutdown);
