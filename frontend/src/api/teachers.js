import axiosClient from "./axiosClient";

export const getTeachers = (status) =>
  axiosClient.get("/teachers", { params: { status } }).then((res) => res.data);
export const getTeacher = (id) => axiosClient.get(`/teachers/${id}`).then((res) => res.data);
export const createTeacher = (data) => axiosClient.post("/teachers", data).then((res) => res.data);
export const updateTeacher = (id, data) => axiosClient.put(`/teachers/${id}`, data).then((res) => res.data);
export const deleteTeacher = (id) => axiosClient.delete(`/teachers/${id}`);
