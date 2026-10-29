import axiosClient from "./axiosClient";

export const getSubjectsByPlan = (planId, semester) =>
  axiosClient.get(`/study-plans/${planId}/subjects`, { params: { semester } }).then((res) => res.data);
export const getSubject = (id) => axiosClient.get(`/subjects/${id}`).then((res) => res.data);
export const createSubject = (planId, data) =>
  axiosClient.post(`/study-plans/${planId}/subjects`, data).then((res) => res.data);
export const updateSubject = (id, data) => axiosClient.put(`/subjects/${id}`, data).then((res) => res.data);
export const deleteSubject = (id) => axiosClient.delete(`/subjects/${id}`);
