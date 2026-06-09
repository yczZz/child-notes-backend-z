/* admin/modules/core/data.js — 共用数据获取 */

import { state, api } from "./state.js";
import { toast } from "./toast.js";

export async function fetchOverview() {
  try {
    Object.assign(state.overview, await api.request("/overview"));
  } catch (e) {
    toast.message?.error(e.message);
  }
}
