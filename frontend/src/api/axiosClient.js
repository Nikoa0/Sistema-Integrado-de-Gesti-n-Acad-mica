import axios from "axios";
import keycloak from "../auth/keycloak";

const axiosClient = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

// Adds the Keycloak JWT to every request, refreshing it first if it is about to expire
axiosClient.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(30);
    } catch {
      keycloak.login();
    }
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      keycloak.login();
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
