import { z } from 'zod';

import type { Session, TokenPair } from '../../domain/account/session';
import type { User } from '../../domain/account/user';

const userSchema = z.object({
  id: z.string(),
  name: z.string(),
  email: z.string(),
});

const sessionSchema = z.object({
  accessToken: z.string(),
  expiresIn: z.number(),
  refreshToken: z.string(),
  user: userSchema,
});

const tokensSchema = z.object({
  accessToken: z.string(),
  expiresIn: z.number(),
  refreshToken: z.string(),
});

/** El usuario de `GET /v1/me`, o null si el cuerpo no cuadra. */
export function readUser(body: unknown): User | null {
  const parsed = userSchema.safeParse(body);
  return parsed.success ? parsed.data : null;
}

/** La sesion de `register` y `login`. */
export function readSession(body: unknown): Session | null {
  const parsed = sessionSchema.safeParse(body);
  if (!parsed.success) return null;
  const { accessToken, expiresIn, refreshToken, user } = parsed.data;
  return { accessToken, expiresInSeconds: expiresIn, refreshToken, user };
}

/** El par rotado de `refresh`. */
export function readTokens(body: unknown): TokenPair | null {
  const parsed = tokensSchema.safeParse(body);
  if (!parsed.success) return null;
  const { accessToken, expiresIn, refreshToken } = parsed.data;
  return { accessToken, expiresInSeconds: expiresIn, refreshToken };
}
