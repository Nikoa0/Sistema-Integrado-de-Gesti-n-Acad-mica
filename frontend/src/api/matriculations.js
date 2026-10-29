import axiosClient from "./axiosClient";

export const getMatriculationsByPeriod = (periodId) =>
  axiosClient.get(`/academic-periods/${periodId}/matriculations`).then((res) => res.data);
export const createMatriculation = (periodId, studentId) =>
  axiosClient.post(`/academic-periods/${periodId}/matriculations`, { studentId }).then((res) => res.data);
export const deleteMatriculation = (id) => axiosClient.delete(`/matriculations/${id}`);
