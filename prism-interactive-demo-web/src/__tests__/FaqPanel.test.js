import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import FaqPanel, { faqs } from '../components/landing/Molecules/FaqPanel/FaqPanel';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<FaqPanel />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<FaqPanel />);

    expect(screen.getByText('landing.faqPanel.title')).toBeTruthy();
    faqs.forEach(({ title, description }) => {
      expect(screen.getByText(title)).toBeTruthy();
      expect(screen.getByText(description)).toBeTruthy();
    });
  });
});
