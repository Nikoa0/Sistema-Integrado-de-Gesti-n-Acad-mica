import axiosClient from "./axiosClient";

export const getCareers = () => axiosClient.get("/careers").then((res) => res.data);
export const getCareer = (id) => axiosClient.get(`/careers/${id}`).then((res) => res.data);
export const createCareer = (data) => axiosClient.post("/careers", data).then((res) => res.data);
export const updateCareer = (id, data) => axiosClient.put(`/careers/${id}`, data).then((res) => res.data);
export const deleteCareer = (id) => axiosClient.delete(`/careers/${id}`);
