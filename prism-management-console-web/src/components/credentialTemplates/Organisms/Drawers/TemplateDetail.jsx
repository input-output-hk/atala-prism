import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { Drawer } from 'antd';
import { drawerWidth } from '../../../../helpers/constants';
import { sanitizeView } from '../../../../helpers/credentialView';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import './_style.scss';

const TemplateDetail = ({ drawerInfo, templateData }) => {
  const { template } = templateData;

  const renderHtmlTemplate = () => {
    const unescapedHtml = _.unescape(template);
    const cleanHtml = sanitizeView(unescapedHtml);
    /* eslint-disable-next-line react/no-danger */
    return <div dangerouslySetInnerHTML={{ __html: cleanHtml }} />;
  };

  return (
    <Drawer
      className="templateDetailDrawer"
      placement="right"
      width={drawerWidth}
      destroyOnClose
      {...drawerInfo}
    >
      <div className="templateContainer">{renderHtmlTemplate()}</div>
    </Drawer>
  );
};

TemplateDetail.defaultProps = {
  templateData: {}
};

TemplateDetail.propTypes = {
  drawerInfo: PropTypes.shape({
    visible: PropTypes.bool,
    onClose: PropTypes.func
  }).isRequired,
  templateData: credentialTypeShape
};

export default TemplateDetail;
