import { useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { createTeacher, deleteTeacher, getTeachers, updateTeacher } from "../../api/teachers";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import Field from "../../components/Field";
import FormModal from "../../components/FormModal";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import { TEACHER_STATUSES } from "../../constants/enums";
import useAction from "../../hooks/useAction";
import useEntityForm from "../../hooks/useEntityForm";
import useFetch from "../../hooks/useFetch";

const EMPTY = {
  run: "",
  fullName: "",
  email: "",
  professionalTitle: "",
  academicDegree: "",
  status: "ACTIVE",
};

export default function TeachersPage() {
  const [statusFilter, setStatusFilter] = useState("");
  const { data, loading, error, reload } = useFetch(
    () => getTeachers(statusFilter || undefined),
    [statusFilter]
  );
  const teachers = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) =>
      editing
        ? updateTeacher(editing.id, {
            fullName: form.fullName,
            email: form.email,
            professionalTitle: form.professionalTitle,
            academicDegree: form.academicDegree,
            status: form.status,
          })
        : createTeacher({
            run: form.run,
            fullName: form.fullName,
            email: form.email,
            professionalTitle: form.professionalTitle,
            academicDegree: form.academicDegree,
          }),
    reload
  );

  const confirmDelete = async () => {
    await action.run(() => deleteTeacher(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Teachers"
        subtitle="Manage teachers"
        actions={<Button onClick={editor.openCreate}>New teacher</Button>}
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <Form.Select
        aria-label="Filter by status"
        className="mb-3"
        style={{ maxWidth: 240 }}
        value={statusFilter}
        onChange={(e) => setStatusFilter(e.target.value)}
      >
        <option value="">All statuses</option>
        {TEACHER_STATUSES.map((s) => (
          <option key={s}>{s}</option>
        ))}
      </Form.Select>

      <QueryState loading={loading} error={error}>
        {teachers.length === 0 ? (
          <p className="text-muted">No teachers match the current filter.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>RUN</th>
                <th>Name</th>
                <th>Email</th>
                <th>Title</th>
                <th>Degree</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {teachers.map((t) => (
                <tr key={t.id}>
                  <td>{t.run}</td>
                  <td>{t.fullName}</td>
                  <td>{t.email}</td>
                  <td>{t.professionalTitle}</td>
                  <td>{t.academicDegree}</td>
                  <td>
                    <StatusBadge status={t.status} />
                  </td>
                  <td className="text-end text-nowrap">
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="me-2"
                      onClick={() =>
                        editor.openEdit(t, {
                          run: t.run,
                          fullName: t.fullName,
                          email: t.email,
                          professionalTitle: t.professionalTitle,
                          academicDegree: t.academicDegree,
                          status: t.status,
                        })
                      }
                    >
                      Edit
                    </Button>
                    <Button size="sm" variant="outline-danger" onClick={() => setToDelete(t)}>
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
        title={editor.editing ? "Edit teacher" : "New teacher"}
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
        <Field label="Email" hint="Must match the email of the teacher's Keycloak user.">
          <Form.Control type="email" required maxLength={150} value={editor.form.email} onChange={editor.setField("email")} />
        </Field>
        <Field label="Professional title">
          <Form.Control
            required
            maxLength={150}
            value={editor.form.professionalTitle}
            onChange={editor.setField("professionalTitle")}
          />
        </Field>
        <Field label="Academic degree">
          <Form.Control
            required
            maxLength={150}
            value={editor.form.academicDegree}
            onChange={editor.setField("academicDegree")}
          />
        </Field>
        {editor.editing && (
          <Field label="Status" hint="A teacher with sections in an OPEN period cannot become INACTIVE.">
            <Form.Select value={editor.form.status} onChange={editor.setField("status")}>
              {TEACHER_STATUSES.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </Form.Select>
          </Field>
        )}
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete teacher"
        message={`Delete ${toDelete?.fullName}? Teachers with sections cannot be deleted.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
