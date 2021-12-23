import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import UseCasesPanel from '../components/landing/Organisms/UseCasesPanel/UseCasesPanel';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

const tabs = ['Education', 'Government', 'Health', 'Enterprise', 'Finance', 'Travel', 'Social'];

describe('<UseCasesPanel />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<UseCasesPanel />);

    expect(screen.getByText('landing.useCasesPanel.title')).toBeTruthy();
    expect(screen.getByText('landing.useCasesPanel.subtitle')).toBeTruthy();

    tabs.forEach(tab => {
      expect(screen.getByText(tab)).toBeTruthy();
    });
  });
});
