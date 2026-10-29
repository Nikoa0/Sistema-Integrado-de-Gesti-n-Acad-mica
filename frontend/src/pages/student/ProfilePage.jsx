import PageHeader from "../../components/PageHeader";

export default function ProfilePage() {
  return (
    <>
      <PageHeader title="My profile" subtitle="Academic information and study plan" />
      {/* TODO: show student data (api/students.js with studentId from useAuth) and the subjects of the study plan (api/subjects.js) */}
    </>
  );
}
