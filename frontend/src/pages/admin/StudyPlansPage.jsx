import { useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { Link } from "react-router-dom";
import { getCareers } from "../../api/careers";
import {
  createStudyPlan,
  deleteStudyPlan,
  getStudyPlansByCareer,
  updateStudyPlan,
} from "../../api/studyPlans";
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

const EMPTY = { identifier: "", current: false };

export default function StudyPlansPage() {
  const careersQuery = useFetch(() => getCareers(), []);
  const careers = careersQuery.data ?? [];
  const [chosenCareerId, setChosenCareerId] = useState("");
  const careerId = chosenCareerId || (careers[0]?.id ?? "");

  const { data, loading, error, reload } = useFetch(
    () => (careerId ? getStudyPlansByCareer(careerId) : Promise.resolve([])),
    [careerId]
  );
  const plans = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form, editing) => {
      const body = { identifier: form.identifier, current: form.current };
      return editing ? updateStudyPlan(editing.id, body) : createStudyPlan(careerId, body);
    },
    reload
  );

  const confirmDelete = async () => {
    await action.run(() => deleteStudyPlan(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Study plans"
        subtitle="Manage the study plans of each career"
        actions={
          <Button onClick={editor.openCreate} disabled={!careerId}>
            New study plan
          </Button>
        }
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={careersQuery.loading} error={careersQuery.error}>
        <Form.Group className="mb-3" style={{ maxWidth: 420 }}>
          <Form.Label>Career</Form.Label>
          <Form.Select value={careerId} onChange={(e) => setChosenCareerId(Number(e.target.value))}>
            {careers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.code} - {c.name}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
      </QueryState>

      <QueryState loading={loading} error={error}>
        {plans.length === 0 ? (
          <p className="text-muted">This career has no study plans yet.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Identifier</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {plans.map((p) => (
                <tr key={p.id}>
                  <td>{p.identifier}</td>
                  <td>
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="text-end text-nowrap">
                    <Button
                      size="sm"
                      variant="outline-secondary"
                      className="me-2"
                      as={Link}
                      to={`/admin/study-plans/${p.id}/subjects`}
                    >
                      Subjects
                    </Button>
                    <Button
                      size="sm"
                      variant="outline-primary"
                      className="me-2"
                      onClick={() =>
                        editor.openEdit(p, { identifier: p.identifier, current: p.status === "CURRENT" })
                      }
                    >
                      Edit
                    </Button>
                    <Button size="sm" variant="outline-danger" onClick={() => setToDelete(p)}>
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
        title={editor.editing ? "Edit study plan" : "New study plan"}
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        <Field label="Identifier" hint="For example 2021.3">
          <Form.Control
            required
            maxLength={20}
            value={editor.form.identifier}
            onChange={editor.setField("identifier")}
          />
        </Field>
        <Form.Check
          type="checkbox"
          id="plan-current"
          label="Make this the current plan of the career (the previous current plan becomes NOT CURRENT)"
          checked={editor.form.current}
          onChange={editor.setField("current")}
        />
      </FormModal>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete study plan"
        message={`Delete the study plan "${toDelete?.identifier}"? It can only be deleted if it has no students or subjects.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
