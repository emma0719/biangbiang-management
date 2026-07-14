import { api, apiBinary } from '../src/api/client';
import { getTokens } from '../src/auth/tokenStore';

jest.mock('../src/auth/tokenStore', () => ({
  getTokens: jest.fn(),
  saveTokens: jest.fn()
}));

describe('api client', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue({ ok: true })
    }) as jest.Mock;
  });

  it('does not attach stale bearer tokens to public login requests', async () => {
    (getTokens as jest.Mock).mockResolvedValue({ accessToken: 'stale-token', refreshToken: 'refresh-token' });

    await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ identifier: 'mary@dev.example.com', password: 'password123' }) });

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/auth/login',
      expect.objectContaining({
        headers: expect.not.objectContaining({ Authorization: expect.any(String) })
      })
    );
  });

  it('attaches bearer tokens to protected API requests', async () => {
    (getTokens as jest.Mock).mockResolvedValue({ accessToken: 'valid-token', refreshToken: 'refresh-token' });

    await api('/api/me');

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/me',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer valid-token' })
      })
    );
  });

  it('fetches protected PDF bytes with bearer headers and no token query parameter', async () => {
    (getTokens as jest.Mock).mockResolvedValue({ accessToken: 'pdf-token', refreshToken: 'refresh-token' });
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: {
        get: jest.fn((name: string) => {
          if (name === 'Content-Type') return 'application/pdf';
          if (name === 'Content-Length') return '4';
          if (name === 'Content-Disposition') return 'inline; filename="order-plan.pdf"';
          return null;
        })
      },
      arrayBuffer: jest.fn().mockResolvedValue(new Uint8Array([37, 80, 68, 70]).buffer)
    }) as jest.Mock;

    const response = await apiBinary('/api/order-plans/70/pdf/view');

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/order-plans/70/pdf/view',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer pdf-token' })
      })
    );
    expect((global.fetch as jest.Mock).mock.calls[0][0]).not.toContain('pdf-token');
    expect(response.mimeType).toBe('application/pdf');
    expect(response.filename).toBe('order-plan.pdf');
    expect(response.byteSize).toBe(4);
  });
});
