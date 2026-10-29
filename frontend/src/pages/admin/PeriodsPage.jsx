import { useState } from "react";
import { Alert, Button, Col, Form, Row, Table } from "react-bootstrap";
import { Link } from "react-router-dom";
import { closePeriod, createPeriod, deletePeriod, getPeriods } from "../../api/academicPeriods";
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

const EMPTY = { year: new Date().getFullYear(), semester: 1 };

export default function PeriodsPage() {
  const { data, loading, error, reload } = useFetch(() => getPeriods(), []);
  const periods = data ?? [];
  const action = useAction();
  const [toDelete, setToDelete] = useState(null);
  const [toClose, setToClose] = useState(null);
  const [closure, setClosure] = useState(null);

  const editor = useEntityForm(
    EMPTY,
    (form) => createPeriod({ year: Number(form.year), semester: Number(form.semester) }),
    reload
  );

  const confirmDelete = async () => {
    await action.run(() => deletePeriod(toDelete.id));
    setToDelete(null);
    reload();
  };

  const confirmClose = async () => {
    const result = await action.run(() => closePeriod(toClose.id));
    setToClose(null);
    if (result) setClosure(result);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Academic periods"
        subtitle="Manage academic periods"
        actions={<Button onClick={editor.openCreate}>New period</Button>}
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      {closure && (
        <Alert variant="success" dismissible onClose={() => setClosure(null)}>
          <Alert.Heading>Period {closure.periodName} closed</Alert.Heading>
          <p className="mb-1">
            Graduated students: {closure.graduatedRuns.length ? closure.graduatedRuns.join(", ") : "none"}
          </p>
          <p className="mb-0">
            Eliminated students: {closure.eliminatedRuns.length ? closure.eliminatedRuns.join(", ") : "none"}
          </p>
        </Alert>
      )}

      <QueryState loading={loading} error={error}>
        {periods.length === 0 ? (
          <p className="text-muted">There are no academic periods yet.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Period</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {periods.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="text-end text-nowrap">
                    <Button as={Link} size="sm" variant="outline-secondary" className="me-2" to={`/admin/periods/${p.id}/sections`}>
                      Sections
                    </Button>
                    <Button as={Link} size="sm" variant="outline-secondary" className="me-2" to={`/admin/periods/${p.id}/matriculations`}>
                      Matriculations
                    </Button>
                    <Button as={Link} size="sm" variant="outline-secondary" className="me-2" to={`/admin/periods/${p.id}/enrollments`}>
                      Enrollments
                    </Button>
                    <Button
                      size="sm"
                      variant="outline-warning"
                      className="me-2"
                      disabled={p.status !== "OPEN"}
                      onClick={() => setToClose(p)}
                    >
                      Close
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
        title="New academic period"
        onHide={editor.close}
        onSubmit={editor.submit}
        saving={editor.saving}
        error={editor.error}
      >
        <Row>
          <Col>
            <Field label="Year">
              <Form.Control type="number" min={2000} required value={editor.form.year} onChange={editor.setField("year")} />
            </Field>
          </Col>
          <Col>
            <Field label="Semester">
              <Form.Select value={editor.form.semester} onChange={editor.setField("semester")}>
                <option value={1}>1</option>
                <option value={2}>2</option>
              </Form.Select>
            </Field>
          </Col>
        </Row>
        <p className="text-muted mb-0">The period starts OPEN.</p>
      </FormModal>

      <ConfirmModal
        show={Boolean(toClose)}
        title="Close academic period"
        variant="warning"
        message={`Close the period ${toClose?.name}? Every enrollment must have a final grade. Student statuses will be updated and grades can no longer be changed.`}
        confirmLabel="Close period"
        onConfirm={confirmClose}
        onCancel={() => setToClose(null)}
      />
      <ConfirmModal
        show={Boolean(toDelete)}
        title="Delete period"
        message={`Delete the period ${toDelete?.name}? It can only be deleted if it has no sections or matriculations.`}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
