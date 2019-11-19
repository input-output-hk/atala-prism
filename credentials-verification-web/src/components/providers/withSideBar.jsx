import React, { Fragment } from 'react';
import Header from '../common/Molecules/Header/Header';
import SideMenu from '../common/Molecules/SideBar/SideBar';

export const withSideBar = Component => props => (
  <Fragment>
    <Header />
    <div className="MainContent">
      <SideMenu />
      <div className="MainContainer">
        <Component {...props} />;
      </div>
    </div>
  </Fragment>
);
