import axiosClient from "./axiosClient";

export const getSectionsByPeriod = (periodId, teacherId) =>
  axiosClient.get(`/academic-periods/${periodId}/sections`, { params: { teacherId } }).then((res) => res.data);
export const getSection = (id) => axiosClient.get(`/sections/${id}`).then((res) => res.data);
export const createSection = (periodId, data) =>
  axiosClient.post(`/academic-periods/${periodId}/sections`, data).then((res) => res.data);
export const updateSection = (id, data) => axiosClient.put(`/sections/${id}`, data).then((res) => res.data);
export const deleteSection = (id) => axiosClient.delete(`/sections/${id}`);
