import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import IntroItem from '../components/landing/Molecules/IntroItem/IntroItem';

const itemTitle = 'Title';
const itemText = 'Text';
const itemIcon = '/images/icon-help.svg';

describe('<IntroItem />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<IntroItem itemIcon={itemIcon} itemText={itemText} itemTitle={itemTitle} />);

    expect(screen.getByAltText('Item Icon')).toBeTruthy();
    expect(screen.getByText(itemTitle)).toBeTruthy();
    expect(screen.getByText(itemText)).toBeTruthy();
  });
});
