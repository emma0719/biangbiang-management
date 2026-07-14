import { getTokens, saveTokens } from '../auth/tokenStore';

const baseUrl = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
const publicAuthPaths = ['/api/auth/login', '/api/auth/refresh', '/api/public/'];

export class ApiError extends Error {
  constructor(public code: string, public status?: number) {
    super(code);
    this.name = 'ApiError';
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetchWithAuth(path, init);
  if (!response.ok) {
    const body = await response.json().catch(() => ({ code: 'NETWORK_ERROR' }));
    throw new ApiError(body.code ?? 'SERVER_ERROR', response.status);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export type ApiBinaryResponse = {
  arrayBuffer: ArrayBuffer;
  blob?: Blob;
  mimeType: string;
  filename?: string;
  byteSize: number;
};

export async function apiBinary(path: string, init: RequestInit = {}): Promise<ApiBinaryResponse> {
  const response = await fetchWithAuth(path, init);
  if (!response.ok) {
    const body = await response.json().catch(() => ({ code: 'NETWORK_ERROR' }));
    throw new ApiError(body.code ?? 'SERVER_ERROR', response.status);
  }
  const mimeType = response.headers?.get?.('Content-Type') ?? 'application/octet-stream';
  const arrayBuffer = await response.arrayBuffer();
  const contentLength = Number(response.headers?.get?.('Content-Length'));
  return {
    arrayBuffer,
    blob: typeof Blob === 'undefined' ? undefined : new Blob([arrayBuffer], { type: mimeType }),
    mimeType,
    filename: parseContentDispositionFilename(response.headers?.get?.('Content-Disposition') ?? undefined),
    byteSize: Number.isFinite(contentLength) && contentLength >= 0 ? contentLength : arrayBuffer.byteLength
  };
}

async function fetchWithAuth(path: string, init: RequestInit = {}): Promise<Response> {
  const { accessToken, refreshToken } = await getTokens();
  const isFormData = typeof FormData !== 'undefined' && init.body instanceof FormData;
  const shouldAttachAccessToken = accessToken && !publicAuthPaths.some((publicPath) => path.startsWith(publicPath));
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(shouldAttachAccessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...init.headers
    }
  });
  if (response.status === 401 && refreshToken) {
    const refreshed = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    if (refreshed.ok) {
      const tokens = await refreshed.json();
      await saveTokens(tokens.accessToken, tokens.refreshToken);
      return fetchWithAuth(path, init);
    }
  }
  return response;
}

function parseContentDispositionFilename(header?: string): string | undefined {
  if (!header) return undefined;
  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (encoded?.[1]) return decodeURIComponent(encoded[1]);
  const quoted = header.match(/filename="([^"]+)"/i);
  if (quoted?.[1]) return quoted[1];
  const plain = header.match(/filename=([^;]+)/i);
  return plain?.[1]?.trim();
}
