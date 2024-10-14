import { schedule } from "node-cron";
import { v4 as createUUID } from "uuid";
import type { Point } from "./messages";
import { publish, subscribe } from "./rabbitmq";

const players = new Map<string, Point>();

const MAX_PLAYERS = 20;

console.log("simulator started");

function addPlayer() {
  const uuid = createUUID();
  console.log(`${uuid} joined the game (${players.size}/${MAX_PLAYERS})`);
  players.set(uuid, {
    x: Math.floor(Math.random() * 200 - 100),
    y: Math.floor(Math.random() * 40 + 100),
    z: Math.floor(Math.random() * 200 - 100),
    world: "overworld",
  });
}

function removePlayer() {
  const index = Math.floor(Math.random() * players.size);
  const uuid = [...players.keys()][index];
  console.log(`${uuid} left the game (${players.size}/${MAX_PLAYERS})`);
  players.delete(uuid);
  publish("PlayerDisconnectMessage", { player: uuid });
}

for (let i = 0; i < MAX_PLAYERS / 2; i++) {
  addPlayer();
}

const interval = process.env.INTERVAL || "*/2 * * * * *";

schedule(
  interval,
  () => {
    if (players.size > 0 && Math.random() < 0.5) {
      removePlayer();
    }

    players.forEach((pos, key) => {
      if (Math.random() > 0.2) return;

      const dx = Math.floor(Math.random() * 24 - 12);
      const dz = Math.floor(Math.random() * 24 - 12);
      players.set(key, { ...pos, x: pos.x + dx, z: pos.z + dz });
    });

    if (players.size < MAX_PLAYERS && Math.random() < 0.5) {
      addPlayer();
    }

    players.forEach((pos, player) => {
      publish("PlayerMoveMessage", { pos, player });
    });
  },
  { runOnInit: true }
);

publish("ServerStatusMessage", { status: "STARTED" });

subscribe("RestartMessage", () => {
  publish("ServerStatusMessage", { status: "RECOVERED" });
});
