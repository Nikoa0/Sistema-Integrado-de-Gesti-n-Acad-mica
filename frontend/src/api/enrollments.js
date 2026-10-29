import axiosClient from "./axiosClient";

export const getOffer = (studentId, periodId) =>
  axiosClient.get(`/students/${studentId}/offer`, { params: { periodId } }).then((res) => res.data);
export const enroll = (studentId, periodId, subjectIds) =>
  axiosClient.post(`/students/${studentId}/enrollments`, { periodId, subjectIds }).then((res) => res.data);
export const getEnrollmentsByStudent = (studentId, periodId) =>
  axiosClient.get(`/students/${studentId}/enrollments`, { params: { periodId } }).then((res) => res.data);
export const getEnrollmentsByPeriod = (periodId) =>
  axiosClient.get(`/academic-periods/${periodId}/enrollments`).then((res) => res.data);
export const getEnrollmentsBySection = (sectionId) =>
  axiosClient.get(`/sections/${sectionId}/enrollments`).then((res) => res.data);
export const cancelEnrollment = (id) => axiosClient.delete(`/enrollments/${id}`);
