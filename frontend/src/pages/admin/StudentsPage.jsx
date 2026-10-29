import { useState } from "react";
import { Button, Col, Form, Row, Table } from "react-bootstrap";
import { getCareers } from "../../api/careers";
import { createStudent, deleteStudent, getStudents, updateStudent } from "../../api/students";
import { getStudyPlansByCareer } from "../../api/studyPlans";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import Field from "../../components/Field";
import FormModal from "../../components/FormModal";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import { MANUAL_STUDENT_STATUSES, STUDENT_STATUSES } from "../../constants/enums";
import useAction from "../../hooks/useAction";
import useEntityForm from "../../hooks/useEntityForm";
import useFetch from "../../hooks/useFetch";

const EMPTY = { run: "", fullName: "", email: "", careerId: "", studyPlanId: "", status: "REGULAR" };

export default function StudentsPage() {
  const [careerFilter, setCareerFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const careersQuery = useFetch(() => getCareers(), []);
  const careers = careersQuery.data ?? [];

  const { data, loading, error, reload } = useFetch(
    () => getStudents({ careerId: careerFilter || undefined, status: statusFilter || undefined }),
    [careerFilter, statusFilter]
  );
  const students = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) =>
      editing
        ? updateStudent(editing.id, {
            fullName: form.fullName,
            email: form.email,
            careerId: Number(form.careerId),
            studyPlanId: form.studyPlanId ? Number(form.studyPlanId) : null,
            status: form.status,
          })
        : createStudent({
            run: form.run,
            fullName: form.fullName,
            email: form.email,
            careerId: Number(form.careerId),
          }),
    reload
  );

  const editingId = editor.editing?.id ?? null;
  const plansQuery = useFetch(
    () =>
      editingId && editor.form.careerId
        ? getStudyPlansByCareer(editor.form.careerId)
        : Promise.resolve([]),
    [editingId, editor.form.careerId]
  );
  const plans = plansQuery.data ?? [];

  const careerOptions = editor.editing ? careers : careers.filter((c) => c.status === "ACTIVE");
  const statusOptions =
    editor.editing && !MANUAL_STUDENT_STATUSES.includes(editor.editing.status)
      ? [editor.editing.status, ...MANUAL_STUDENT_STATUSES]
      : MANUAL_STUDENT_STATUSES;

  const confirmDelete = async () => {
    await action.run(() => deleteStudent(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Students"
        subtitle="Manage students"
        actions={<Button onClick={editor.openCreate}>New student</Button>}
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <Row className="mb-3 g-2" style={{ maxWidth: 700 }}>
        <Col sm={6}>
          <Form.Select aria-label="Filter by career" value={careerFilter} onChange={(e) => setCareerFilter(e.target.value)}>
            <option value="">All careers</option>
            {careers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Form.Select>
        </Col>
        <Col sm={6}>
          <Form.Select aria-label="Filter by status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            {STUDENT_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replaceAll("_", " ")}
              </option>
            ))}
          </Form.Select>
        </Col>
      </Row>

      <QueryState loading={loading} error={error}>
        {students.length === 0 ? (
          <p className="text-muted">No students match the current filters.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>RUN</th>
                <th>Name</th>
                <th>Email</th>
                <th>Career</th>
                <th>Plan</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td>{s.run}</td>
                  <td>{s.fullName}</td>
                  <td>{s.email}</td>
                  <td>{s.careerName}</td>
                  <td>{s.studyPlanIdentifier}</td>
                  <td>
                    <StatusBadge status={s.status} />
                  </td>
                  <td className="text-end text-nowrap">
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="me-2"
                      onClick={() =>
                        editor.openEdit(s, {
                          run: s.run,
                          fullName: s.fullName,
                          email: s.email,
                          careerId: String(s.careerId),
                          studyPlanId: String(s.studyPlanId),
                          status: s.status,
                        })
                      }
                    >
                      Edit
                    </Button>
                    <Button size="sm" variant="outline-danger" onClick={() => setToDelete(s)}>
                      Delete
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </QueryState>

      <FormModal
        show={editor.show}
        title={editor.editing ? "Edit student" : "New student"}
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        <Field label="RUN" hint="Format 12345678-9">
          <Form.Control
            required
            pattern="\d{7,8}-[\dkK]"
            disabled={Boolean(editor.editing)}
            value={editor.form.run}
            onChange={editor.setField("run")}
          />
        </Field>
        <Field label="Full name">
          <Form.Control required maxLength={150} value={editor.form.fullName} onChange={editor.setField("fullName")} />
        </Field>
        <Field label="Email" hint="Must match the email of the student's Keycloak user.">
          <Form.Control type="email" required maxLength={150} value={editor.form.email} onChange={editor.setField("email")} />
        </Field>
        <Field
          label="Career"
          hint={
            editor.editing
              ? "Career and plan can only change while the student has no enrollments or history."
              : "The student is assigned to the current study plan of the career."
          }
        >
          <Form.Select
            required
            value={editor.form.careerId}
            onChange={(e) => editor.setForm((f) => ({ ...f, careerId: e.target.value, studyPlanId: "" }))}
          >
            <option value="">Select a career</option>
            {careerOptions.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Form.Select>
        </Field>
        {editor.editing && (
          <>
            <Field label="Study plan">
              <Form.Select value={editor.form.studyPlanId} onChange={editor.setField("studyPlanId")}>
                <option value="">Current plan of the career (automatic)</option>
                {plans.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.identifier}
                    {p.status === "CURRENT" ? " (current)" : ""}
                  </option>
                ))}
              </Form.Select>
            </Field>
            <Field label="Status" hint="GRADUATED and ELIMINATED are assigned automatically when a period is closed.">
              <Form.Select value={editor.form.status} onChange={editor.setField("status")}>
                {statusOptions.map((s) => (
                  <option key={s} value={s}>
                    {s.replaceAll("_", " ")}
                  </option>
                ))}
              </Form.Select>
            </Field>
          </>
        )}
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete student"
        message={`Delete ${toDelete?.fullName}? Students with enrollments or academic history cannot be deleted.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
