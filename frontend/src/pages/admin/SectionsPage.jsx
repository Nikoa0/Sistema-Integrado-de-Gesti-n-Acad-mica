import { useState } from "react";
import { Button, Col, Form, Row, Table } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import { getPeriod } from "../../api/academicPeriods";
import { getCareers } from "../../api/careers";
import { createSection, deleteSection, getSectionsByPeriod, updateSection } from "../../api/sections";
import { getStudyPlansByCareer } from "../../api/studyPlans";
import { getSubjectsByPlan } from "../../api/subjects";
import { getTeachers } from "../../api/teachers";
import BlockGrid from "../../components/BlockGrid";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import Field from "../../components/Field";
import FormModal from "../../components/FormModal";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useAction from "../../hooks/useAction";
import useEntityForm from "../../hooks/useEntityForm";
import useFetch from "../../hooks/useFetch";

const EMPTY = { subjectId: "", teacherId: "", capacity: 30, blocks: [] };

// Everything the page needs, loaded at once. Subjects of every study plan are offered to the admin.
async function loadAll(periodId) {
  const [period, sections, teachers, careers] = await Promise.all([
    getPeriod(periodId),
    getSectionsByPeriod(periodId),
    getTeachers(),
    getCareers(),
  ]);
  const plans = (
    await Promise.all(
      careers.map((c) => getStudyPlansByCareer(c.id).then((list) => list.map((p) => ({ ...p, careerName: c.name }))))
    )
  ).flat();
  const subjects = (
    await Promise.all(
      plans.map((p) =>
        getSubjectsByPlan(p.id).then((list) => list.map((s) => ({ ...s, planLabel: `${p.careerName} ${p.identifier}` })))
      )
    )
  ).flat();
  return { period, sections, teachers, subjects };
}

export default function SectionsPage() {
  const { periodId } = useParams();
  const { data, loading, error, reload } = useFetch(() => loadAll(periodId), [periodId]);
  const period = data?.period ?? null;
  const sections = (data?.sections ?? []).slice().sort((a, b) => a.subjectCode.localeCompare(b.subjectCode));
  const subjects = data?.subjects ?? [];
  const teachers = data?.teachers ?? [];
  const isOpen = period?.status === "OPEN";

  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) => {
      const common = { teacherId: Number(form.teacherId), capacity: Number(form.capacity), blocks: form.blocks };
      return editing
        ? updateSection(editing.id, common)
        : createSection(periodId, { subjectId: Number(form.subjectId), ...common });
    },
    reload
  );

  const form = editor.form;
  const currentSubjectId = editor.editing ? editor.editing.subjectId : Number(form.subjectId);
  const currentSubject = subjects.find((s) => s.id === currentSubjectId);
  const requiredBlocks = currentSubject
    ? (currentSubject.theoryHours + currentSubject.exerciseHours + currentSubject.labHours) / 2
    : null;
  const freeSubjects = subjects.filter((s) => !sections.some((x) => x.subjectId === s.id));
  const teacherOptions = teachers.filter((t) => t.status === "ACTIVE" || t.id === Number(form.teacherId));

  const confirmDelete = async () => {
    await action.run(() => deleteSection(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title={period ? `Sections - ${period.name}` : "Sections"}
        subtitle="Academic offer of a period"
        actions={
          <>
            <Button as={Link} to="/admin/periods" variant="outline-secondary" className="me-2">
              Back
            </Button>
            <Button onClick={editor.openCreate} disabled={!isOpen}>
              New section
            </Button>
          </>
        }
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={loading} error={error}>
        {period && (
          <p>
            Period status: <StatusBadge status={period.status} />
            {!isOpen && <span className="text-muted ms-2">The offer of a CLOSED period cannot be modified.</span>}
          </p>
        )}
        {sections.length === 0 ? (
          <p className="text-muted">This period has no sections yet.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Subject</th>
                <th>Teacher</th>
                <th>Capacity</th>
                <th>Schedule</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {sections.map((s) => (
                <tr key={s.id}>
                  <td>
                    {s.subjectCode} - {s.subjectName}
                  </td>
                  <td>{s.teacherName}</td>
                  <td>{s.capacity}</td>
                  <td>{s.blocks.join(", ")}</td>
                  <td className="text-end text-nowrap">
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="me-2"
                      disabled={!isOpen}
                      onClick={() =>
                        editor.openEdit(s, {
                          subjectId: String(s.subjectId),
                          teacherId: String(s.teacherId),
                          capacity: s.capacity,
                          blocks: s.blocks,
                        })
                      }
                    >
                      Edit
                    </Button>
                    <Button size="sm" variant="outline-danger" disabled={!isOpen} onClick={() => setToDelete(s)}>
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
        size="lg"
        title={editor.editing ? `Edit section of ${editor.editing.subjectCode}` : "New section"}
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        {!editor.editing && (
          <Field label="Subject" hint="Only subjects without a section in this period are listed.">
            <Form.Select required value={form.subjectId} onChange={editor.setField("subjectId")}>
              <option value="">Select a subject</option>
              {freeSubjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code} - {s.name} ({s.planLabel})
                </option>
              ))}
            </Form.Select>
          </Field>
        )}
        <Row>
          <Col md={8}>
            <Field label="Teacher" hint="Only ACTIVE teachers can be assigned.">
              <Form.Select required value={form.teacherId} onChange={editor.setField("teacherId")}>
                <option value="">Select a teacher</option>
                {teacherOptions.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.fullName}
                  </option>
                ))}
              </Form.Select>
            </Field>
          </Col>
          <Col md={4}>
            <Field label="Capacity" hint="1 to 60">
              <Form.Control
                type="number"
                min={1}
                max={60}
                required
                value={form.capacity}
                onChange={editor.setField("capacity")}
              />
            </Field>
          </Col>
        </Row>
        <Field
          label="Schedule"
          hint={
            requiredBlocks === null
              ? "Select a subject to know how many blocks it needs."
              : `This subject needs ${requiredBlocks} block(s) per week (2 hours each). Selected: ${form.blocks.length}.`
          }
        >
          <BlockGrid value={form.blocks} onChange={(blocks) => editor.setForm((f) => ({ ...f, blocks }))} />
        </Field>
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete section"
        message={`Delete the section of ${toDelete?.subjectCode}? Sections with enrolled students cannot be deleted.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
