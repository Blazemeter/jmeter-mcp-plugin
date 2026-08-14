import { serveStdio } from '@modelcontextprotocol/server/stdio';
import { factory } from './factory.mjs';

const strict = process.argv.includes('--strict');
serveStdio(factory, strict ? { legacy: 'reject' } : undefined);
