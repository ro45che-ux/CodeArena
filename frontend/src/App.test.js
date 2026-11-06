import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

beforeEach(() => localStorage.clear());

test('shows the login form when there is no session', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: /codearena/i })).toBeInTheDocument();
  expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /access platform/i })).toBeInTheDocument();
});

test('switches to the registration form with a password hint', () => {
  render(<App />);
  userEvent.click(screen.getByText(/sign up/i));
  expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/password \(min\. 8 characters\)/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
});
