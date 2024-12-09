import { useEffect } from 'react';

const GoogleAd = () => {
    useEffect(() => {
        try {
            (window.adsbygoogle = window.adsbygoogle || []).push({});
        } catch (err) {
            console.error('AdSense 에러:', err);
        }
    }, []);

    return (
        <ins className="adsbygoogle"
             style={{ display: 'block' }}
             data-ad-client="ca-pub-2739842765144901"
             data-ad-slot="1318513227"
             data-ad-format="auto"
             data-full-width-responsive="true">
        </ins>
    );
};

export default GoogleAd;
