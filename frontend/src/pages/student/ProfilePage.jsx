import { Alert, Badge, Col, Row, Table } from "react-bootstrap";
import { getEnrollmentsByStudent } from "../../api/enrollments";
import { getStudent } from "../../api/students";
import { getSubjectsByPlan } from "../../api/subjects";
import { useAuth } from "../../auth/useAuth";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useFetch from "../../hooks/useFetch";

const SEMESTERS = [1, 2, 3, 4];

export default function ProfilePage() {
  const { studentId } = useAuth();
  const student = useFetch(() => (studentId ? getStudent(studentId) : Promise.resolve(null)), [studentId]);
  const planId = student.data?.studyPlanId ?? null;
  const subjectsQuery = useFetch(() => (planId ? getSubjectsByPlan(planId) : Promise.resolve([])), [planId]);
  const history = useFetch(() => (studentId ? getEnrollmentsByStudent(studentId) : Promise.resolve([])), [studentId]);

  const subjects = subjectsQuery.data ?? [];
  const approved = new Set((history.data ?? []).filter((e) => e.result === "APPROVED").map((e) => e.subjectId));
  const s = student.data;

  if (!studentId) {
    return (
      <>
        <PageHeader title="My profile" subtitle="Academic information and study plan" />
        <Alert variant="warning">No student record is linked to your user. Ask the administrator to check your email.</Alert>
      </>
    );
  }

  return (
    <>
      <PageHeader title="My profile" subtitle="Academic information and study plan" />
      <QueryState loading={student.loading} error={student.error}>
        {s && (
          <Row className="mb-4">
            <Col md={6}>
              <dl className="row mb-0">
                <dt className="col-sm-4">Name</dt>
                <dd className="col-sm-8">{s.fullName}</dd>
                <dt className="col-sm-4">RUN</dt>
                <dd className="col-sm-8">{s.run}</dd>
                <dt className="col-sm-4">Email</dt>
                <dd className="col-sm-8">{s.email}</dd>
              </dl>
            </Col>
            <Col md={6}>
              <dl className="row mb-0">
                <dt className="col-sm-4">Career</dt>
                <dd className="col-sm-8">{s.careerName}</dd>
                <dt className="col-sm-4">Study plan</dt>
                <dd className="col-sm-8">{s.studyPlanIdentifier}</dd>
                <dt className="col-sm-4">Status</dt>
                <dd className="col-sm-8">
                  <StatusBadge status={s.status} />
                </dd>
              </dl>
            </Col>
          </Row>
        )}
      </QueryState>

      <h4>Study plan</h4>
      <QueryState loading={subjectsQuery.loading} error={subjectsQuery.error}>
        {SEMESTERS.map((semester) => {
          const rows = subjects.filter((x) => x.semester === semester).sort((a, b) => a.code.localeCompare(b.code));
          if (rows.length === 0) return null;
          return (
            <div key={semester} className="mb-3">
              <h6>Semester {semester}</h6>
              <Table striped size="sm" responsive className="align-middle">
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>TEL</th>
                    <th>SCT</th>
                    <th>Prerequisites</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((x) => (
                    <tr key={x.id}>
                      <td>{x.code}</td>
                      <td>{x.name}</td>
                      <td>
                        {x.theoryHours}-{x.exerciseHours}-{x.labHours}
                      </td>
                      <td>{x.sct}</td>
                      <td>{x.prerequisiteCodes.length ? x.prerequisiteCodes.join(", ") : "-"}</td>
                      <td>{approved.has(x.id) && <Badge bg="success">Approved</Badge>}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          );
        })}
      </QueryState>
    </>
  );
}
