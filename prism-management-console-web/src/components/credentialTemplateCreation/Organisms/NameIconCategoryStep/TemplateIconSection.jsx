import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import IconPickerModal from '../IconPickerModal/IconPickerModal';
import { useTemplateSketch } from '../../../../hooks/useTemplateSketch';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const TemplateNameSection = observer(() => {
  const { templateSketch } = useTemplateSketch();
  const [showIconPickerModal, setShowIconPickerModal] = useState(false);

  return (
    <div className="TemplateIconContainer flex">
      <div className="TemplateIconWrapper">
        <img className="TemplateIcon" src={templateSketch.icon} alt="template icon" />
      </div>
      <div className="verticalFlex">
        {/* TODO: add i18n */}
        <p>Temple Icon</p>
        <p>Select an Icon for your template</p>
        <CustomButton
          buttonProps={{
            onClick: () => setShowIconPickerModal(true)
          }}
          // TODO: add i18n
          buttonText="Change Icon"
        />
      </div>

      <IconPickerModal visible={showIconPickerModal} close={() => setShowIconPickerModal(false)} />
    </div>
  );
});

export default TemplateNameSection;
