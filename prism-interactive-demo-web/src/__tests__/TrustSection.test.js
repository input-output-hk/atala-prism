import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import TrustSection, { items } from '../components/landing/Organisms/TrustSection/TrustSection';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<TrustSection />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<TrustSection />);

    expect(screen.getByText('landing.trust.predownloadText')).toBeTruthy();

    items.forEach(({ title, description }) => {
      expect(screen.getByText(title)).toBeTruthy();
      expect(screen.getByText(description)).toBeTruthy();
    });
  });
});
