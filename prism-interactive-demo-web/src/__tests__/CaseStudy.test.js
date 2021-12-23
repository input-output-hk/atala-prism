import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import CaseStudy from '../components/landing/Molecules/CaseStudy/CaseStudy';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<CaseStudy />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<CaseStudy />);

    expect(screen.getByText('landing.caseStudy.title')).toBeTruthy();
    expect(screen.getByText('landing.caseStudy.part1')).toBeTruthy();
    expect(screen.getByText('landing.caseStudy.part2')).toBeTruthy();
    expect(screen.getByAltText('landing.caseStudy.caseStudyAlt')).toBeTruthy();
  });
});
