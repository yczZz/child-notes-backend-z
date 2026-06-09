/* admin/modules/core/state.js — 全局响应式状态 */

import { createApi } from "./api.js";

const { reactive, ref } = Vue;
const TOKEN_KEY = "child_notes_admin_token";
const USERNAME_KEY = "child_notes_admin_username";
const DISPLAY_KEY = "child_notes_admin_display_name";

/* ---- token ---- */
const token = ref(localStorage.getItem(TOKEN_KEY) || "");

/* ---- overview ---- */
const overview = reactive({
  totalUsers: 0, todayUsers: 0, totalBabies: 0, todayBabies: 0,
  draftLotteryCount: 0, publishedLotteryCount: 0,
});

/* ---- users ---- */
const users = reactive({
  page: 1, pageSize: 20, total: 0, keyword: "", records: [],
});

/* ---- babies ---- */
const babies = reactive({
  page: 1, pageSize: 20, total: 0, keyword: "", records: [],
});

/* ---- lotteries ---- */
const lotteries = reactive({
  page: 1, pageSize: 20, total: 0, status: "all", records: [],
});

/* ---- login form ---- */
const loginForm = reactive({ username: "admin", password: "" });

/* ---- loading ---- */
const loading = reactive({
  login: false, users: false, babies: false, lotteries: false, save: false,
});

/* ---- session helpers ---- */
function clearSession() {
  token.value = "";
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(DISPLAY_KEY);
}

function persistSession(data) {
  token.value = data.token;
  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USERNAME_KEY, data.username || "");
  localStorage.setItem(DISPLAY_KEY, data.displayName || "");
}

function savedUsername() {
  return localStorage.getItem(USERNAME_KEY) || "admin";
}

/* ---- singleton exports ---- */
export const state = {
  token, overview, users, babies, lotteries, loginForm, loading,
  clearSession, persistSession, savedUsername,
};

export const api = createApi(token);
