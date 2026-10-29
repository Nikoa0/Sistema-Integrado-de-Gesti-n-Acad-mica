import axiosClient from "./axiosClient";

export const registerGrade = (enrollmentId, grade) =>
  axiosClient.put(`/enrollments/${enrollmentId}/grade`, { grade }).then((res) => res.data);
