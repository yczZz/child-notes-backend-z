/* admin/modules/core/api.js — HTTP 请求封装 */

const BASE = "/admin/api";

export function createApi(tokenRef) {
  async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (tokenRef.value) {
      headers.Authorization = `Bearer ${tokenRef.value}`;
    }
    const resp = await fetch(`${BASE}${path}`, { ...options, headers });
    const payload = await resp.json().catch(() => null);
    if (resp.status === 401) {
      throw new Error("login expired");
    }
    if (!payload || payload.state !== "000000") {
      throw new Error(payload?.msg || "request failed");
    }
    return payload.data;
  }

  return { request };
}
