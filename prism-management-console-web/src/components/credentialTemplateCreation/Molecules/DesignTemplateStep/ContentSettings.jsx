import React from 'react';
import HeaderEditor from '../../Atoms/DesignTemplateStep/HeaderEditor';
import BodyEditor from '../../Atoms/DesignTemplateStep/BodyEditor';
import './_style.scss';

const ContentSettings = props => (
  <div className="contentContainer">
    <div>
      <HeaderEditor />
    </div>
    <div>
      <BodyEditor />
    </div>
  </div>
);

ContentSettings.propTypes = {};

export default ContentSettings;
