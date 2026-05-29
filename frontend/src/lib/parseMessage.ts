// Sanctioned boundary for WebSocket message parsing.
// `as T` is intentional here — the caller declares the expected shape.
export function parseMessage<T>(msg: { body: string }): T {
  return JSON.parse(msg.body) as T;
}
