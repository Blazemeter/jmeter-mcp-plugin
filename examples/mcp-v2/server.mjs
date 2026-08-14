import { createMcpExpressApp } from '@modelcontextprotocol/express';
import { toNodeHandler } from '@modelcontextprotocol/node';
import { createMcpHandler } from '@modelcontextprotocol/server';
import { factory } from './factory.mjs';

function listen(label, handler, port) {
  const app = createMcpExpressApp();
  const node = toNodeHandler(handler);
  app.all('/mcp', (req, res) => void node(req, res, req.body));
  return new Promise((resolve) => {
    const httpServer = app.listen(port, '127.0.0.1', () => {
      console.log(`${label} listening on http://127.0.0.1:${port}/mcp`);
      resolve(httpServer);
    });
  });
}

const dualPort = Number(process.env.DUAL_PORT || 18080);
const strictPort = Number(process.env.STRICT_PORT || 18081);

await listen('dual', createMcpHandler(factory), dualPort);
await listen('strict', createMcpHandler(factory, { legacy: 'reject' }), strictPort);
