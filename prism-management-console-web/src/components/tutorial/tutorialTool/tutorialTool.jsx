import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Menu } from 'antd';
import ProgressChecklist from './ProgressChecklist.jsx';
import { AppstoreOutlined, MailOutlined, SettingOutlined } from '@ant-design/icons';
import './_style.scss';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton.jsx';

const { SubMenu } = Menu;

const rootSubmenuKeys = ['sub1', 'sub2', 'sub4'];

const TutorialTool = () => {

  const { t } = useTranslation();

  const [openKeys, setOpenKeys] = React.useState(['sub1']);

  const onOpenChange = keys => {
    const latestOpenKey = keys.find(key => openKeys.indexOf(key) === -1);
    if (rootSubmenuKeys.indexOf(latestOpenKey) === -1) {
      setOpenKeys(keys);
    } else {
      setOpenKeys(latestOpenKey ? [latestOpenKey] : []);
    }
  };

  return (
    <div className="tutorialContainer">
      <Menu mode="inline" openKeys={openKeys} onOpenChange={onOpenChange} style={{ width: 256 }}>
        <SubMenu key="sub1" title="Tutorial Checklist">
          {/* Each progressChecklist should link to each page and activate the tutorial on each page */}
          <ProgressChecklist text={t('tutorial.tutorialChecklist.stepName.basicSteps')} percent={50} />
          <ProgressChecklist text={t('tutorial.tutorialChecklist.stepName.contacts')} percent={10} />
          <ProgressChecklist text={t('tutorial.tutorialChecklist.stepName.groups')} percent={30} />
          <ProgressChecklist text={t('tutorial.tutorialChecklist.stepName.credentials')} percent={50} />
          {/* this should close the tool. And should be accesed again on the side bar */}
          <Menu.Item><CustomButton
        buttonProps={{
          className: 'theme-outline',
        }}
        buttonText={t('tutorial.tutorialChecklist.skip')}
      /></Menu.Item>
        </SubMenu>
      </Menu>
    </div>
  );
};

export default TutorialTool;
