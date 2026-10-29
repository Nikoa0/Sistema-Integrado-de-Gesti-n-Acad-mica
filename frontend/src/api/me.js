import axiosClient from "./axiosClient";

export const getMe = () => axiosClient.get("/me").then((res) => res.data);
