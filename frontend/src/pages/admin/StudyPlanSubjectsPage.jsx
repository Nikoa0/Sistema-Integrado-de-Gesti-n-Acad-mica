import { useState } from "react";
import { Alert, Button, Col, Form, Row, Table } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import { getStudyPlan } from "../../api/studyPlans";
import { createSubject, deleteSubject, getSubjectsByPlan, updateSubject } from "../../api/subjects";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import Field from "../../components/Field";
import FormModal from "../../components/FormModal";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import useAction from "../../hooks/useAction";
import useEntityForm from "../../hooks/useEntityForm";
import useFetch from "../../hooks/useFetch";

const SEMESTERS = [1, 2, 3, 4];
const MAX_PREREQUISITES = 3;
const EMPTY = {
  code: "",
  name: "",
  semester: 1,
  theoryHours: 2,
  exerciseHours: 0,
  labHours: 2,
  sct: 5,
  prerequisiteCodes: [],
};

export default function StudyPlanSubjectsPage() {
  const { planId } = useParams();
  const plan = useFetch(() => getStudyPlan(planId), [planId]);
  const { data, loading, error, reload } = useFetch(() => getSubjectsByPlan(planId), [planId]);
  const subjects = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) => {
      const body = {
        code: form.code,
        name: form.name,
        semester: Number(form.semester),
        theoryHours: Number(form.theoryHours),
        exerciseHours: Number(form.exerciseHours),
        labHours: Number(form.labHours),
        sct: Number(form.sct),
        prerequisiteCodes: form.prerequisiteCodes,
      };
      return editing ? updateSubject(editing.id, body) : createSubject(planId, body);
    },
    reload
  );

  const form = editor.form;
  const weeklyHours = Number(form.theoryHours) + Number(form.exerciseHours) + Number(form.labHours);
  const candidates = subjects
    .filter((s) => s.semester < Number(form.semester) && s.code !== form.code)
    .sort((a, b) => a.code.localeCompare(b.code));

  const changeSemester = (e) => {
    const semester = Number(e.target.value);
    editor.setForm((f) => ({
      ...f,
      semester,
      prerequisiteCodes: f.prerequisiteCodes.filter((code) =>
        subjects.some((s) => s.code === code && s.semester < semester)
      ),
    }));
  };

  const togglePrerequisite = (code) =>
    editor.setForm((f) => {
      const selected = f.prerequisiteCodes.includes(code);
      if (!selected && f.prerequisiteCodes.length >= MAX_PREREQUISITES) return f;
      return {
        ...f,
        prerequisiteCodes: selected
          ? f.prerequisiteCodes.filter((c) => c !== code)
          : [...f.prerequisiteCodes, code],
      };
    });

  const confirmDelete = async () => {
    await action.run(() => deleteSubject(toDelete.id));
    setToDelete(null);
    reload();
  };

  const title = plan.data ? `Subjects - plan ${plan.data.identifier}` : "Subjects";

  return (
    <>
      <PageHeader
        title={title}
        subtitle="Subjects and prerequisites of a study plan"
        actions={
          <>
            <Button as={Link} to="/admin/study-plans" variant="outline-secondary" className="me-2">
              Back
            </Button>
            <Button onClick={editor.openCreate}>New subject</Button>
          </>
        }
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={loading} error={error}>
        {subjects.length === 0 && <p className="text-muted">This plan has no subjects yet.</p>}
        {SEMESTERS.map((semester) => {
          const rows = subjects.filter((s) => s.semester === semester).sort((a, b) => a.code.localeCompare(b.code));
          if (rows.length === 0) return null;
          return (
            <div key={semester} className="mb-4">
              <h5>Semester {semester}</h5>
              <Table striped hover responsive size="sm" className="align-middle">
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
                  {rows.map((s) => (
                    <tr key={s.id}>
                      <td>{s.code}</td>
                      <td>{s.name}</td>
                      <td>
                        {s.theoryHours}-{s.exerciseHours}-{s.labHours}
                      </td>
                      <td>{s.sct}</td>
                      <td>{s.prerequisiteCodes.length ? s.prerequisiteCodes.join(", ") : "-"}</td>
                      <td className="text-end text-nowrap">
                        <Button
                          size="sm"
                          variant="outline-primary"
                          className="me-2"
                          onClick={() =>
                            editor.openEdit(s, {
                              code: s.code,
                              name: s.name,
                              semester: s.semester,
                              theoryHours: s.theoryHours,
                              exerciseHours: s.exerciseHours,
                              labHours: s.labHours,
                              sct: s.sct,
                              prerequisiteCodes: s.prerequisiteCodes,
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
            </div>
          );
        })}
      </QueryState>

      <FormModal
        show={editor.show}
        size="lg"
        title={editor.editing ? "Edit subject" : "New subject"}
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        <Row>
          <Col md={4}>
            <Field label="Code">
              <Form.Control required maxLength={20} value={form.code} onChange={editor.setField("code")} />
            </Field>
          </Col>
          <Col md={8}>
            <Field label="Name">
              <Form.Control required maxLength={150} value={form.name} onChange={editor.setField("name")} />
            </Field>
          </Col>
        </Row>
        <Row>
          <Col md={3}>
            <Field label="Semester">
              <Form.Select value={form.semester} onChange={changeSemester}>
                {SEMESTERS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </Form.Select>
            </Field>
          </Col>
          {[
            ["theoryHours", "Theory h"],
            ["exerciseHours", "Exercise h"],
            ["labHours", "Lab h"],
          ].map(([name, label]) => (
            <Col md={3} key={name}>
              <Field label={label}>
                <Form.Control type="number" min={0} required value={form[name]} onChange={editor.setField(name)} />
              </Field>
            </Col>
          ))}
        </Row>
        <Row>
          <Col md={3}>
            <Field label="SCT" hint="1 to 7">
              <Form.Control type="number" min={1} max={7} required value={form.sct} onChange={editor.setField("sct")} />
            </Field>
          </Col>
          <Col md={9}>
            {weeklyHours >= 8 ? (
              <Alert variant="warning" className="py-2">
                Weekly hours (TEL) add up to {weeklyHours}; the total must be lower than 8.
              </Alert>
            ) : (
              <p className="text-muted mt-4 mb-0">
                Weekly hours: {weeklyHours} ({weeklyHours % 2 === 0 ? weeklyHours / 2 : "odd"} schedule blocks)
              </p>
            )}
          </Col>
        </Row>
        <Field label={`Prerequisites (up to ${MAX_PREREQUISITES}, earlier semesters only)`}>
          {candidates.length === 0 ? (
            <p className="text-muted mb-0">No subjects in earlier semesters.</p>
          ) : (
            <div style={{ maxHeight: 160, overflowY: "auto" }} className="border rounded p-2">
              {candidates.map((s) => (
                <Form.Check
                  key={s.id}
                  type="checkbox"
                  id={`prereq-${s.id}`}
                  label={`${s.code} - ${s.name}`}
                  checked={form.prerequisiteCodes.includes(s.code)}
                  onChange={() => togglePrerequisite(s.code)}
                />
              ))}
            </div>
          )}
        </Field>
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete subject"
        message={`Delete the subject "${toDelete?.code} - ${toDelete?.name}"?`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
