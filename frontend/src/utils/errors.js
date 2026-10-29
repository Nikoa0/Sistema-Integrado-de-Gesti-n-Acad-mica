// Turns an Axios error into a message that can be shown to the user.
// The backend answers business errors as { timestamp, status, message }.
export function getErrorMessage(error) {
  const response = error?.response;
  if (response?.data?.message) {
    return response.data.message;
  }
  if (!response) {
    return "Cannot reach the server. Please try again later.";
  }
  if (response.status === 401) {
    return "Your session has expired. Please log in again.";
  }
  if (response.status === 403) {
    return "You do not have permission to perform this action.";
  }
  return "Unexpected error. Please try again.";
}
