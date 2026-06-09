const TOKEN_KEY = "child_notes_admin_token";
const USERNAME_KEY = "child_notes_admin_username";
const DISPLAY_KEY = "child_notes_admin_display_name";

export function mountAdminFallback(root, reason) {
  const state = {
    token: localStorage.getItem(TOKEN_KEY) || "",
    username: localStorage.getItem(USERNAME_KEY) || "admin",
    displayName: localStorage.getItem(DISPLAY_KEY) || "",
    view: "dashboard",
    reason,
    page: { users: 1, babies: 1, lotteries: 1 },
    pageSize: 20,
    keyword: { users: "", babies: "" },
    lotteryStatus: "all",
    cache: {
      overview: null,
      users: { total: 0, records: [] },
      babies: { total: 0, records: [] },
      lotteries: { total: 0, records: [] },
    },
  };

  const api = createApi(state, () => renderLogin());

  function renderLogin(message) {
    root.innerHTML = `
      <main class="fallback-login">
        <form class="fallback-panel" id="loginForm">
          <h1>Admin Console</h1>
          ${state.reason ? `<p class="fallback-note">Fallback UI: ${escapeHtml(state.reason)}</p>` : ""}
          ${message ? `<p class="fallback-error">${escapeHtml(message)}</p>` : ""}
          <label>Username</label>
          <input name="username" value="${escapeAttr(state.username || "admin")}" autocomplete="username" />
          <label>Password</label>
          <input name="password" type="password" autocomplete="current-password" autofocus />
          <button type="submit">Login</button>
        </form>
      </main>
    `;
    root.querySelector("#loginForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = new FormData(event.currentTarget);
      try {
        const data = await api.request("/auth/login", {
          method: "POST",
          body: JSON.stringify({
            username: form.get("username"),
            password: form.get("password"),
          }),
        });
        persistSession(data);
        state.view = "dashboard";
        renderShell();
        await loadCurrentView();
      } catch (error) {
        renderLogin(error.message);
      }
    });
  }

  function renderShell() {
    root.innerHTML = `
      <div class="fallback-shell">
        <aside class="fallback-sidebar">
          <div class="fallback-brand">Admin Console</div>
          ${navButton("dashboard", "Overview")}
          ${navButton("users", "Users")}
          ${navButton("babies", "Babies")}
          ${navButton("lotteries", "Lotteries")}
        </aside>
        <main class="fallback-main">
          <header class="fallback-topbar">
            <h1>${activeTitle()}</h1>
            <div>
              <span>${escapeHtml(state.displayName || state.username || "admin")}</span>
              <button class="fallback-secondary" id="logoutBtn">Logout</button>
            </div>
          </header>
          <section class="fallback-content" id="fallbackContent"></section>
        </main>
      </div>
    `;
    root.querySelectorAll("[data-view]").forEach((button) => {
      button.addEventListener("click", async () => {
        state.view = button.dataset.view;
        renderShell();
        await loadCurrentView();
      });
    });
    root.querySelector("#logoutBtn").addEventListener("click", async () => {
      try {
        await api.request("/auth/logout", { method: "POST" });
      } catch (_) {
        // Token may already be expired. Local cleanup is still correct.
      }
      clearSession();
      renderLogin();
    });
    renderLoading();
  }

  async function loadCurrentView() {
    if (state.view === "dashboard") {
      await loadOverview();
    } else if (state.view === "users") {
      await loadUsers(state.page.users);
    } else if (state.view === "babies") {
      await loadBabies(state.page.babies);
    } else if (state.view === "lotteries") {
      await loadLotteries(state.page.lotteries);
    }
  }

  async function loadOverview() {
    try {
      state.cache.overview = await api.request("/overview");
      renderOverview();
    } catch (error) {
      renderError(error.message);
    }
  }

  function renderOverview() {
    const o = state.cache.overview || {};
    content().innerHTML = `
      <div class="fallback-metrics">
        ${metric("Total Users", o.totalUsers)}
        ${metric("New Users Today", o.todayUsers)}
        ${metric("Total Babies", o.totalBabies)}
        ${metric("New Babies Today", o.todayBabies)}
        ${metric("Draft Lotteries", o.draftLotteryCount)}
        ${metric("Published Lotteries", o.publishedLotteryCount)}
      </div>
    `;
  }

  async function loadUsers(page) {
    state.page.users = page || 1;
    renderLoading();
    try {
      state.cache.users = await api.request(`/users?page=${state.page.users}&pageSize=${state.pageSize}&keyword=${encodeURIComponent(state.keyword.users)}`);
      renderUsers();
    } catch (error) {
      renderError(error.message);
    }
  }

  function renderUsers() {
    const data = state.cache.users;
    content().innerHTML = `
      <div class="fallback-card">
        ${toolbar("users", "User nickname", state.keyword.users)}
        <div class="fallback-table-wrap">
          <table class="fallback-table">
            <thead><tr><th>ID</th><th>Nickname</th><th>Babies</th><th>Referrer</th><th>Created At</th></tr></thead>
            <tbody>
              ${data.records.map((row) => `
                <tr>
                  <td>${row.id || ""}</td>
                  <td>${escapeHtml(row.nickName || "")}</td>
                  <td>${row.babyCount || 0}</td>
                  <td>${row.referrerUserId || ""}</td>
                  <td>${escapeHtml(row.createdAt || "")}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
        ${pager("users", data.total)}
      </div>
    `;
    bindSearch("users", loadUsers);
    bindPager("users", loadUsers);
  }

  async function loadBabies(page) {
    state.page.babies = page || 1;
    renderLoading();
    try {
      state.cache.babies = await api.request(`/babies?page=${state.page.babies}&pageSize=${state.pageSize}&keyword=${encodeURIComponent(state.keyword.babies)}`);
      renderBabies();
    } catch (error) {
      renderError(error.message);
    }
  }

  function renderBabies() {
    const data = state.cache.babies;
    content().innerHTML = `
      <div class="fallback-card">
        ${toolbar("babies", "Baby name", state.keyword.babies)}
        <div class="fallback-table-wrap">
          <table class="fallback-table">
            <thead><tr><th>ID</th><th>Name</th><th>Gender</th><th>Birth Date</th><th>Age Days</th><th>Owner</th><th>Members</th><th>Created At</th></tr></thead>
            <tbody>
              ${data.records.map((row) => `
                <tr>
                  <td>${row.id || ""}</td>
                  <td>${escapeHtml(row.name || "")}</td>
                  <td>${escapeHtml(row.gender || "")}</td>
                  <td>${escapeHtml(row.birthDate || "")}</td>
                  <td>${row.ageDays ?? ""}</td>
                  <td>${escapeHtml(row.ownerNickName || "")}</td>
                  <td>${row.memberCount || 0}</td>
                  <td>${escapeHtml(row.createdAt || "")}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
        ${pager("babies", data.total)}
      </div>
    `;
    bindSearch("babies", loadBabies);
    bindPager("babies", loadBabies);
  }

  async function loadLotteries(page) {
    state.page.lotteries = page || 1;
    renderLoading();
    try {
      state.cache.lotteries = await api.request(`/lotteries?page=${state.page.lotteries}&pageSize=${state.pageSize}&status=${encodeURIComponent(state.lotteryStatus)}`);
      renderLotteries();
    } catch (error) {
      renderError(error.message);
    }
  }

  function renderLotteries() {
    const data = state.cache.lotteries;
    content().innerHTML = `
      <div class="fallback-card">
        <div class="fallback-toolbar">
          <select id="lotteryStatus">
            ${option("all", "All", state.lotteryStatus)}
            ${option("draft", "Draft", state.lotteryStatus)}
            ${option("published", "Published", state.lotteryStatus)}
            ${option("closed", "Closed", state.lotteryStatus)}
          </select>
          <button id="newLotteryBtn">New Lottery</button>
        </div>
        <div class="fallback-table-wrap">
          <table class="fallback-table">
            <thead><tr><th>ID</th><th>Title</th><th>Draw Time</th><th>Points</th><th>Winners</th><th>Status</th><th>Prizes</th><th>Actions</th></tr></thead>
            <tbody>
              ${data.records.map((row) => `
                <tr>
                  <td>${row.id || ""}</td>
                  <td>${escapeHtml(row.title || "")}</td>
                  <td>${escapeHtml(row.drawTime || "")}</td>
                  <td>${row.costPoints || 0}</td>
                  <td>${row.winnerCount || 1}</td>
                  <td>${escapeHtml(row.status || "")}</td>
                  <td>${escapeHtml((row.prizes || []).map((p) => p.prizeName).join(", "))}</td>
                  <td class="fallback-actions">
                    <button data-edit="${row.id}">Edit</button>
                    ${row.status !== "published" ? `<button data-publish="${row.id}">Publish</button>` : ""}
                    ${row.status !== "closed" ? `<button data-close="${row.id}">Close</button>` : ""}
                  </td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
        ${pager("lotteries", data.total)}
      </div>
    `;
    content().querySelector("#lotteryStatus").addEventListener("change", (event) => {
      state.lotteryStatus = event.target.value;
      loadLotteries(1);
    });
    content().querySelector("#newLotteryBtn").addEventListener("click", () => renderLotteryDialog());
    content().querySelectorAll("[data-edit]").forEach((button) => {
      button.addEventListener("click", () => {
        const row = state.cache.lotteries.records.find((item) => String(item.id) === button.dataset.edit);
        renderLotteryDialog(row);
      });
    });
    content().querySelectorAll("[data-publish]").forEach((button) => {
      button.addEventListener("click", () => changeLotteryStatus(button.dataset.publish, "publish"));
    });
    content().querySelectorAll("[data-close]").forEach((button) => {
      button.addEventListener("click", () => changeLotteryStatus(button.dataset.close, "close"));
    });
    bindPager("lotteries", loadLotteries);
  }

  function renderLotteryDialog(row) {
    const form = row ? normalizeLottery(row) : emptyLottery();
    const modal = document.createElement("div");
    modal.className = "fallback-modal";
    modal.innerHTML = `
      <form class="fallback-dialog" id="lotteryForm">
        <h2>${form.id ? "Edit Lottery" : "New Lottery"}</h2>
        <label>Title</label>
        <input name="title" value="${escapeAttr(form.title)}" required />
        <label>Description</label>
        <textarea name="description" rows="3">${escapeHtml(form.description)}</textarea>
        <label>Cover Image</label>
        <input name="coverImage" value="${escapeAttr(form.coverImage)}" />
        <div class="fallback-form-grid">
          <label>Start Time<input name="startTime" type="datetime-local" value="${escapeAttr(toDatetimeLocal(form.startTime))}" required /></label>
          <label>Draw Time<input name="drawTime" type="datetime-local" value="${escapeAttr(toDatetimeLocal(form.drawTime))}" required /></label>
          <label>Cost Points<input name="costPoints" type="number" min="0" value="${form.costPoints}" /></label>
          <label>Winner Count<input name="winnerCount" type="number" min="1" value="${form.winnerCount}" /></label>
          <label>Status
            <select name="status">
              ${option("draft", "Draft", form.status)}
              ${option("published", "Published", form.status)}
              ${option("closed", "Closed", form.status)}
            </select>
          </label>
        </div>
        <label>Prizes</label>
        <div id="prizeRows">
          ${form.prizes.map((prize) => prizeInputs(prize)).join("")}
        </div>
        <button type="button" class="fallback-secondary" id="addPrizeBtn">Add Prize</button>
        <div class="fallback-dialog-footer">
          <button type="button" class="fallback-secondary" id="cancelLotteryBtn">Cancel</button>
          <button type="submit">Save</button>
        </div>
      </form>
    `;
    document.body.appendChild(modal);
    modal.querySelector("#cancelLotteryBtn").addEventListener("click", () => modal.remove());
    modal.querySelector("#addPrizeBtn").addEventListener("click", () => {
      modal.querySelector("#prizeRows").insertAdjacentHTML("beforeend", prizeInputs({ prizeName: "", prizeIntro: "", prizeCount: 1 }));
      bindPrizeRemove(modal);
    });
    bindPrizeRemove(modal);
    modal.querySelector("#lotteryForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const body = collectLotteryForm(event.currentTarget);
      try {
        await api.request(form.id ? `/lotteries/${form.id}` : "/lotteries", {
          method: form.id ? "PUT" : "POST",
          body: JSON.stringify(body),
        });
        modal.remove();
        await loadLotteries(state.page.lotteries);
      } catch (error) {
        alert(error.message);
      }
    });
  }

  function bindPrizeRemove(scope) {
    scope.querySelectorAll("[data-remove-prize]").forEach((button) => {
      button.onclick = () => {
        const rows = scope.querySelectorAll(".fallback-prize-row");
        if (rows.length > 1) {
          button.closest(".fallback-prize-row").remove();
        }
      };
    });
  }

  function collectLotteryForm(form) {
    const prizes = [...form.querySelectorAll(".fallback-prize-row")]
      .map((row) => ({
        prizeName: row.querySelector("[name=prizeName]").value.trim(),
        prizeIntro: row.querySelector("[name=prizeIntro]").value.trim(),
        prizeImage: "",
        prizeCount: Number(row.querySelector("[name=prizeCount]").value || 1),
      }))
      .filter((prize) => prize.prizeName);
    return {
      title: form.title.value.trim(),
      description: form.description.value.trim(),
      coverImage: form.coverImage.value.trim(),
      startTime: toApiDateTime(form.startTime.value),
      drawTime: toApiDateTime(form.drawTime.value),
      costPoints: Number(form.costPoints.value || 0),
      winnerCount: Number(form.winnerCount.value || 1),
      status: form.status.value,
      prizes,
    };
  }

  async function changeLotteryStatus(id, action) {
    try {
      await api.request(`/lotteries/${id}/${action}`, { method: "POST" });
      await loadLotteries(state.page.lotteries);
    } catch (error) {
      alert(error.message);
    }
  }

  function persistSession(data) {
    state.token = data.token || "";
    state.username = data.username || "";
    state.displayName = data.displayName || "";
    localStorage.setItem(TOKEN_KEY, state.token);
    localStorage.setItem(USERNAME_KEY, state.username);
    localStorage.setItem(DISPLAY_KEY, state.displayName);
  }

  function clearSession() {
    state.token = "";
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(DISPLAY_KEY);
  }

  function navButton(key, label) {
    return `<button class="${state.view === key ? "active" : ""}" data-view="${key}">${label}</button>`;
  }

  function activeTitle() {
    return { dashboard: "Overview", users: "Users", babies: "Babies", lotteries: "Lotteries" }[state.view] || "Admin";
  }

  function metric(label, value) {
    return `<div class="fallback-metric"><span>${label}</span><strong>${value ?? 0}</strong></div>`;
  }

  function toolbar(type, placeholder, value) {
    return `
      <form class="fallback-toolbar" data-search="${type}">
        <input name="keyword" placeholder="${placeholder}" value="${escapeAttr(value)}" />
        <button type="submit">Search</button>
      </form>
    `;
  }

  function pager(type, total) {
    const page = state.page[type];
    const pages = Math.max(1, Math.ceil((total || 0) / state.pageSize));
    return `
      <div class="fallback-pager">
        <button data-page-prev="${type}" ${page <= 1 ? "disabled" : ""}>Prev</button>
        <span>Page ${page} / ${pages}, Total ${total || 0}</span>
        <button data-page-next="${type}" ${page >= pages ? "disabled" : ""}>Next</button>
      </div>
    `;
  }

  function bindSearch(type, loader) {
    content().querySelector(`[data-search="${type}"]`).addEventListener("submit", (event) => {
      event.preventDefault();
      state.keyword[type] = new FormData(event.currentTarget).get("keyword") || "";
      loader(1);
    });
  }

  function bindPager(type, loader) {
    const prev = content().querySelector(`[data-page-prev="${type}"]`);
    const next = content().querySelector(`[data-page-next="${type}"]`);
    prev?.addEventListener("click", () => loader(Math.max(1, state.page[type] - 1)));
    next?.addEventListener("click", () => loader(state.page[type] + 1));
  }

  function renderLoading() {
    content().innerHTML = `<div class="fallback-card">Loading...</div>`;
  }

  function renderError(message) {
    content().innerHTML = `<div class="fallback-card fallback-error">${escapeHtml(message)}</div>`;
  }

  function content() {
    return root.querySelector("#fallbackContent");
  }

  if (state.token) {
    renderShell();
    loadCurrentView();
  } else {
    renderLogin();
  }
}

function createApi(state, onUnauthorized) {
  async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.token) {
      headers.Authorization = `Bearer ${state.token}`;
    }
    const response = await fetch(`/admin/api${path}`, { ...options, headers });
    const payload = await response.json().catch(() => null);
    if (response.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      onUnauthorized();
      throw new Error("Login expired");
    }
    if (!payload || payload.state !== "000000") {
      throw new Error(payload?.msg || "Request failed");
    }
    return payload.data;
  }
  return { request };
}

function emptyLottery() {
  const now = new Date();
  const draw = new Date(now.getTime() + 7 * 86400000);
  return {
    id: null,
    title: "",
    description: "",
    coverImage: "",
    startTime: now,
    drawTime: draw,
    costPoints: 0,
    winnerCount: 1,
    status: "draft",
    prizes: [{ prizeName: "", prizeIntro: "", prizeCount: 1 }],
  };
}

function normalizeLottery(row) {
  return {
    id: row.id,
    title: row.title || "",
    description: row.description || "",
    coverImage: row.coverImage || "",
    startTime: row.startTime,
    drawTime: row.drawTime,
    costPoints: row.costPoints || 0,
    winnerCount: row.winnerCount || 1,
    status: row.status || "draft",
    prizes: row.prizes?.length ? row.prizes : [{ prizeName: "", prizeIntro: "", prizeCount: 1 }],
  };
}

function prizeInputs(prize) {
  return `
    <div class="fallback-prize-row">
      <input name="prizeName" placeholder="Prize name" value="${escapeAttr(prize.prizeName || "")}" />
      <input name="prizeIntro" placeholder="Description" value="${escapeAttr(prize.prizeIntro || "")}" />
      <input name="prizeCount" type="number" min="1" value="${prize.prizeCount || 1}" />
      <button type="button" class="fallback-secondary" data-remove-prize>Remove</button>
    </div>
  `;
}

function option(value, label, selected) {
  return `<option value="${escapeAttr(value)}" ${value === selected ? "selected" : ""}>${escapeHtml(label)}</option>`;
}

function toDatetimeLocal(value) {
  if (!value) {
    return "";
  }
  const date = value instanceof Date ? value : new Date(String(value).replace(" ", "T"));
  const pad = (num) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toApiDateTime(value) {
  return value ? `${value}:00` : null;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function escapeAttr(value) {
  return escapeHtml(value);
}
