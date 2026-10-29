import axiosClient from "./axiosClient";

export const getStudents = (params) => axiosClient.get("/students", { params }).then((res) => res.data);
export const getStudent = (id) => axiosClient.get(`/students/${id}`).then((res) => res.data);
export const createStudent = (data) => axiosClient.post("/students", data).then((res) => res.data);
export const updateStudent = (id, data) => axiosClient.put(`/students/${id}`, data).then((res) => res.data);
export const deleteStudent = (id) => axiosClient.delete(`/students/${id}`);
