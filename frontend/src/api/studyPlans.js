import axiosClient from "./axiosClient";

export const getStudyPlansByCareer = (careerId) =>
  axiosClient.get(`/careers/${careerId}/study-plans`).then((res) => res.data);
export const getStudyPlan = (id) => axiosClient.get(`/study-plans/${id}`).then((res) => res.data);
export const createStudyPlan = (careerId, data) =>
  axiosClient.post(`/careers/${careerId}/study-plans`, data).then((res) => res.data);
export const updateStudyPlan = (id, data) =>
  axiosClient.put(`/study-plans/${id}`, data).then((res) => res.data);
export const deleteStudyPlan = (id) => axiosClient.delete(`/study-plans/${id}`);
