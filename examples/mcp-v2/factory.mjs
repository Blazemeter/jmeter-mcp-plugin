import { McpServer } from '@modelcontextprotocol/server';
import { z } from 'zod';

export function factory() {
  const server = new McpServer({ name: 'jmeter-mcp-v2-probe', version: '2.0.0' });
  server.registerTool(
    'echo',
    {
      description: 'Echo a message',
      inputSchema: z.object({ message: z.string() }),
    },
    async ({ message }) => ({
      content: [{ type: 'text', text: message }],
    }),
  );
  return server;
}
