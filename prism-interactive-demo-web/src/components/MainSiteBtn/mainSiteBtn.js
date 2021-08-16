import * as React from 'react';
import hamburger from '../../images/menu.svg';
import MainBtn from '../mainBtn/mainBtn';
import { Popover } from 'antd';
import './_style.scss';

const content = <MainBtn />;

const MainSiteBtn = () => {
  return (
    <div className="mainSiteBtn">
      <Popover content={content}>
        <img src={hamburger} alt="hamburger" />
      </Popover>
    </div>
  );
};

export default MainSiteBtn;
