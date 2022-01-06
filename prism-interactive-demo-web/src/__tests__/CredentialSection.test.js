import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import CredentialSection, {
  items
} from '../components/landing/Organisms/CredentialSection/CredentialSection';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<CredentialSection />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<CredentialSection />);

    expect(screen.getByText('landing.credential.title')).toBeTruthy();

    items.forEach(({ title, description }) => {
      expect(screen.getByText(title)).toBeTruthy();
      expect(screen.getByText(description)).toBeTruthy();
    });

    expect(screen.getAllByText('landing.credential.title7').length).toBe(2);

    expect(screen.getAllByAltText('landing.credential.credentialAlt')).toBeTruthy();
  });
});
