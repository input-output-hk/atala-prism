import React from 'react';
import HeaderEditor from '../../Atoms/DesignTemplateStep/HeaderEditor';
import BodyEditor from '../../Atoms/DesignTemplateStep/BodyEditor';
import './_style.scss';

const ContentSettings = () => (
  <div className="contentContainer">
    <div>
      <HeaderEditor />
    </div>
    <div>
      <BodyEditor />
    </div>
  </div>
);

export default ContentSettings;
