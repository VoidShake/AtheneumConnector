import amqplib from "amqplib";

const EXCHANGE = "exchange";

const connection = await amqplib.connect("amqp://localhost:5672");

const channel = await connection.createChannel();

await channel.assertExchange(EXCHANGE, "direct", { durable: false });

const { queue } = await channel.assertQueue("simulator");

type Message = Record<string, unknown>;

export async function subscribe(
  topic: string,
  handler: (subject: Message) => void | Promise<void>
) {
  await channel.bindQueue(queue, EXCHANGE, topic);

  await channel.consume(queue, async (message) => {
    if (message) {
      const json = JSON.parse(message.content.toString());
      await handler(json);
      channel.ack(message);
    }
  });
}

export async function publish(topic: string, data: Message) {
  channel.publish(EXCHANGE, topic, Buffer.from(JSON.stringify(data)));
}
