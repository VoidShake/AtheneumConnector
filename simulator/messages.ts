export type Point = {
  x: number;
  y: number;
  z: number;
  world: string;
};

export class PlayerMoveMessage {
  constructor(readonly player: string, readonly pos: Point) {}
}
