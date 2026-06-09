/* admin/modules/auth/login.js */

import { state, api } from "../core/state.js";
import { toast } from "../core/toast.js";

const { ref } = Vue;

export default {
  template: `
  <div class="login-wrapper">
    <n-card class="login-card" title="宝宝手记 · 后台管理" size="large" bordered>
      <template #header-extra>
        <n-tag type="info" size="small" round>Admin</n-tag>
      </template>

      <n-form :model="form" label-placement="top">
        <n-form-item label="账号" path="username">
          <n-input
            v-model:value="form.username"
            placeholder="admin"
            clearable
            autocomplete="username"
          />
        </n-form-item>

        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            show-password-on="click"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </n-form-item>

        <n-button
          type="primary"
          size="large"
          block
          :loading="loading"
          attr-type="submit"
          @click="handleLogin"
        >登 录</n-button>
      </n-form>
    </n-card>
  </div>
  `,

  emits: ["loggedIn"],

  setup(props, { emit }) {
    const form = state.loginForm;
    const loading = ref(false);

    async function handleLogin() {
      loading.value = true;
      try {
        const data = await api.request("/auth/login", {
          method: "POST",
          body: JSON.stringify(form),
        });
        state.persistSession(data);
        emit("loggedIn");
      } catch (e) {
        toast.message?.error(e.message);
      } finally {
        loading.value = false;
      }
    }

    return { form, loading, handleLogin };
  },
};
