import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ItemCollapse from '../components/landing/Molecules/ItemCollapse/ItemCollapse';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

describe('<ItemCollapse />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<ItemCollapse name="test" />);

    const collapsableTitle = screen.getByText('landing.intro.itemIcon.readMore');
    expect(collapsableTitle).toBeTruthy();

    userEvent.click(collapsableTitle);

    expect(screen.getByText('landing.intro.itemIcon.test.bullet1')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.test.bullet2')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.test.bullet3')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.test.bullet4')).toBeTruthy();
    expect(screen.getByText('landing.intro.itemIcon.test.bullet5')).toBeTruthy();
  });
});
