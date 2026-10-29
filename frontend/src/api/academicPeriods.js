import axiosClient from "./axiosClient";

export const getPeriods = () => axiosClient.get("/academic-periods").then((res) => res.data);
export const getPeriod = (id) => axiosClient.get(`/academic-periods/${id}`).then((res) => res.data);
export const createPeriod = (data) => axiosClient.post("/academic-periods", data).then((res) => res.data);
export const closePeriod = (id) => axiosClient.post(`/academic-periods/${id}/close`).then((res) => res.data);
export const deletePeriod = (id) => axiosClient.delete(`/academic-periods/${id}`);
